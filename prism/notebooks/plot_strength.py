import os

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns
from pandas.api.types import CategoricalDtype

from util import collect_csv_files
from util import get_plotting_arguments_parser
from util import get_related_configuration
from util import make_dprint

PRIOR_STRENGTH = "Prior Strength ($\\underline{n}$-$\\overline{n}$)"


def main(
    results_path,
    remove_legend,
    external_legend,
    show,
    output_type,
    save,
    confidence_interval,
    out_prefix,
    verbose,
    includes,
    excludes,
):
    if external_legend:
        raise NotImplementedError("can't generate external_legend")
    settings = {}
    settings["prefix"] = results_path.rstrip("/")
    settings['title'] = os.path.basename(settings['prefix']).title().replace("_", " ")
    settings["out_prefix"] = out_prefix if out_prefix is not None else settings['prefix']
    os.makedirs(os.path.dirname(settings["out_prefix"]), exist_ok=True)
    dprint = make_dprint(settings['prefix'], verbose)
    opt_style = {
        "linewidth": 2,
        "color": 'black',
        "linestyle": 'dotted'
    }
    plt.style.use('./paper.mplstyle')
    csv_files = collect_csv_files(includes, excludes, settings["prefix"], verbose)
    alg_names = sorted([os.path.splitext(os.path.split(f)[1])[0] for f in csv_files])

    # reduce memory usage, setting types explicitly
    alg_type = CategoricalDtype(alg_names, ordered=True)
    dtype_map = {
        'Algorithm': alg_type,
        'Accumulated Samples': 'int32',
        'Seed': 'int32',
        'Episode': 'int32',
        'Performance': 'float32',
        'Estimated Performance': 'float32',
        'Average Distance': 'float32',
        'Lower Bound': 'float32',
        'Upper Bound': 'float32',
    }

    # update settings with arbitrary yaml file
    config = get_related_configuration(csv_files[0])
    settings.update(config)
    col_list = [
        'Episode',
        'Performance',
        'Estimated Performance',
        'Average Distance',
        # 'Accumulated Samples'
    ]
    add_legend = not remove_legend
    dfs = []
    for ind, csv_file in enumerate(csv_files):
        config = get_related_configuration(csv_file)
        # if config['seed'] > 1:
        #     continue
        dprint(f"reading {csv_file}")
        new_df = pd.read_csv(
            csv_file,
            usecols=col_list,  # read subset of columns
            memory_map=True,   # load file to memory before processing it
            dtype=dtype_map,   # set dtypes to reduce memory usage
        )
        new_df["Algorithm"] = config["algorithm"]
        new_df["Seed"] = config["seed"]
        new_df.rename(columns={"Average Distance": "Model Error", "Episode": "Trajectory"}, inplace=True)
        new_df[PRIOR_STRENGTH] = "{:02}-{}".format(config["initLowerStrength"], config["initUpperStrength"])
        dfs.append(new_df)

    df = pd.concat(dfs, ignore_index=True)
    dprint(df.columns)
    df.reset_index(inplace=True, drop=True)

    df["Estimation Error"] = df["Estimated Performance"] - df["Performance"]


    # interpolate data in each unique episode
    episodes = sorted(set(list(df['Trajectory'].unique()) + [config["iterations"]]))
    index_columns = ['Algorithm', 'Seed', PRIOR_STRENGTH,  'Trajectory']
    new_index = pd.MultiIndex.from_product([df['Algorithm'].unique(), df['Seed'].unique(), df[PRIOR_STRENGTH].unique(), episodes],
                                           names=index_columns)
    dprint(df.index)
    df = df.set_index(index_columns).reindex(
        new_index).sort_index().reset_index().interpolate(method="pad")

    dprint(df.columns)

    dprint("plotting Performance")
    ax = sns.lineplot(data=df, x="Trajectory", y="Performance", hue=PRIOR_STRENGTH, ci=confidence_interval)
    ax.hlines(y=settings["trueOpt"], xmin=0, xmax=settings["iterations"], **opt_style)
    plt.legend(loc='lower right', title=PRIOR_STRENGTH)
    ax.set_xscale('log')
    if settings["spec"].startswith("R"):
        ax.set_ylabel("$\mathrm{R}^M_\pi(\lozenge \, \ensuremath{T})$", rotation=0, ha='right')
    elif settings["spec"].startswith("P"):
        ax.set_ylabel("$\mathbb{P}^M_\pi(\lozenge \, \ensuremath{T})$", rotation=0, ha='right')
    if save:
        ax.figure.savefig(f"{settings['out_prefix']}.{output_type}")
    if show:
        plt.show()
    dprint("done")


if __name__ == '__main__':
    parser = get_plotting_arguments_parser()
    main(**vars(parser.parse_args()))

import os
from math import log10

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
from pandas.api.types import CategoricalDtype

from util import collect_csv_files
from util import export_legend
from util import get_plotting_arguments_parser
from util import get_related_configuration
from util import make_dprint

UPPER_BOUND = "$n_\\textrm{Max}$"
SWITCHING = "$\\dag$"


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
        num_points=300
):
    add_legend = not remove_legend
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
    alg_names = ["LUI", "MAP", "UCRL"]
    alg_type = CategoricalDtype(alg_names, ordered=True)
    dtype_map = {
        'Algorithm': alg_type,
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
    ]
    config2 = get_related_configuration(csv_files[0].replace('.csv', '_part_2.csv'))
    settings['trueOpt2'] = config2['trueOpt']
    settings['total_trajectories'] = config['iterations'] + config2['iterations']

    episodes = [0, 101, 1001, 10001, 100001]
    episodes += list(np.unique(np.logspace(1, log10(settings['total_trajectories']), num=num_points, dtype='int')))
    # episodes += [settings['total_trajectories']]
    episodes = pd.DataFrame({"Trajectory": sorted(episodes)})
    dfs = []
    for ind, csv_file in enumerate(csv_files):
        config = get_related_configuration(csv_file)
        dprint(f"reading {csv_file}")

        new_df = pd.read_csv(
            csv_file,
            usecols=col_list,  # read subset of columns
            memory_map=True,  # load file to memory before processing it
            dtype=dtype_map,  # set dtypes to reduce memory usage
        )
        new_df.rename(columns={"Average Distance": "Model Error", "Episode": "Trajectory"}, inplace=True)
        new_df["Seed"] = config["seed"]
        new_df["Algorithm"] = config["algorithm"]
        new_df[SWITCHING] = config["iterations"]
        new_df[SWITCHING] = new_df[SWITCHING].astype('int')
        lb = config["lowerStrengthBound"] if config["lowerStrengthBound"] < 10000 else "$\infty$"
        ub = config["upperStrengthBound"] if config["upperStrengthBound"] < 10000 else "$\infty$"
        new_df[UPPER_BOUND] = f"[{lb}, {ub}]"

        new_df = (
            new_df
            .append(episodes, ignore_index=True)
            .drop_duplicates(subset="Trajectory", keep='first')
            .sort_values("Trajectory")
            .interpolate(method="pad")
        )
        new_df = new_df[new_df["Trajectory"].isin(episodes["Trajectory"])].reset_index(drop=True)
        dfs.append(new_df)
    df = pd.concat(dfs)

    df["Estimation Error"] = df["Estimated Performance"] - df["Performance"]
    df["Algorithm"] = df["Algorithm"].astype(alg_type)
    df[SWITCHING] = df[SWITCHING].astype('int')

    df = df.reset_index()
    print(df[UPPER_BOUND].unique())

    def annotate(data, **kws):
        switching_point = data[SWITCHING].unique()[0]
        ax = plt.gca()
        ax.plot([0, switching_point], [settings['trueOpt'], settings['trueOpt']], **opt_style)
        ax.plot([switching_point, settings['total_trajectories']], [settings['trueOpt2'], settings['trueOpt2']], **opt_style)
        # ax.axvline(x=switching_point)
        # ax.text(.1, .6, f"N = {n}", transform=ax.transAxes)

    def plot_grid(col, hue, y="Performance"):
        if len(df[UPPER_BOUND].unique()) > 1:
            row = UPPER_BOUND
            row_order = ['[20, 30]', '[200, 300]', '[$\\infty$, $\\infty$]']
        else :
            row = None
            row_order = None
        g = sns.FacetGrid(df, col=col, hue=hue, row=row, row_order=row_order)

        g.map(sns.lineplot, "Trajectory", y, ci=confidence_interval)
        g.add_legend(adjust_subtitles=True)
        if y == "Performance":
            g.map_dataframe(annotate)
            if settings["spec"].startswith("R"):
                g.set_ylabels("$\mathrm{R}^M_\pi(\lozenge \, \ensuremath{T})$", rotation=0, ha='right')
            elif settings["spec"].startswith("P"):
                g.set_ylabels("$\mathbb{P}^M_\pi(\lozenge \, \ensuremath{T})$", rotation=0, ha='right')
        g.set(xscale='log')
        suf = y.lower().replace(" ", "_")
        if external_legend:
            export_legend(g.axes.flat[0], f"{settings['out_prefix']}_{suf}", output_type, save, show)
        if not add_legend:
            g.figure.legend().remove()
        if save:
            g.savefig(f"{settings['out_prefix']}_{suf}.{output_type}")

    plot_grid(col=SWITCHING, hue="Algorithm")
    plot_grid(col=SWITCHING, hue="Algorithm", y="Model Error")

    if show:
        plt.show()
    dprint("done")


if __name__ == '__main__':
    parser = get_plotting_arguments_parser()
    main(**vars(parser.parse_args()))
